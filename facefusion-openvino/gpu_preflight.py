#!/usr/bin/env python3
import os
import sys

import numpy as np
import onnxruntime as ort
import openvino as ov
from onnx import TensorProto, helper

RENDER_NODE = os.environ.get("FACEFUSION_RENDER_NODE", "/dev/dri/renderD128")


def fail(message: str, code: int = 2) -> None:
    print(f"GPU PREFLIGHT: FAIL - {message}", flush=True)
    raise SystemExit(code)


print("============================================================", flush=True)
print("FaceFusion OpenVINO GPU preflight", flush=True)
print("============================================================", flush=True)
print("Render node:", RENDER_NODE, flush=True)

if not os.path.exists(RENDER_NODE):
    fail(f"{RENDER_NODE} does not exist")

print("ONNX Runtime:", ort.__version__, flush=True)
print("ORT available providers:", ort.get_available_providers(), flush=True)

if "OpenVINOExecutionProvider" not in ort.get_available_providers():
    fail("OpenVINOExecutionProvider is not installed")

core = ov.Core()
print("OpenVINO:", ov.__version__, flush=True)
print("OpenVINO devices:", core.available_devices, flush=True)

gpu_devices = [device for device in core.available_devices if device == "GPU" or device.startswith("GPU.")]
if not gpu_devices:
    fail("native OpenVINO cannot see an Intel GPU")

for device in core.available_devices:
    try:
        name = core.get_property(device, "FULL_DEVICE_NAME")
    except Exception as exc:
        name = f"<unavailable: {exc}>"
    print(f"{device}: {name}", flush=True)

x = helper.make_tensor_value_info("input", TensorProto.FLOAT, [1, 3, 64, 64])
y = helper.make_tensor_value_info("output", TensorProto.FLOAT, [1, 3, 64, 64])
node = helper.make_node("Relu", ["input"], ["output"])
graph = helper.make_graph([node], "facefusion_gpu_probe", [x], [y])
model = helper.make_model(graph, opset_imports=[helper.make_opsetid("", 18)])
model.ir_version = 10

providers = [
    (
        "OpenVINOExecutionProvider",
        {
            "device_type": "GPU",
            "precision": "FP32",
        },
    )
]

try:
    session = ort.InferenceSession(model.SerializeToString(), providers=providers)
except Exception as exc:
    fail(f"failed to create OpenVINO GPU session: {exc}")

actual_providers = session.get_providers()
print("Session providers:", actual_providers, flush=True)
print("Provider options:", session.get_provider_options(), flush=True)

if not actual_providers or actual_providers[0] != "OpenVINOExecutionProvider":
    fail("OpenVINO GPU provider did not initialize; refusing CPU fallback")

data = np.random.default_rng(0).standard_normal((1, 3, 64, 64)).astype(np.float32)
output = session.run(None, {"input": data})[0]
expected = np.maximum(data, 0)

if not np.allclose(output, expected, rtol=1e-5, atol=1e-6):
    fail("GPU inference result verification failed", 3)

print("Inference verification: PASS", flush=True)
print("GPU PREFLIGHT: PASS", flush=True)
print("OpenVINOExecutionProvider is active on Intel GPU.", flush=True)
print("============================================================", flush=True)
