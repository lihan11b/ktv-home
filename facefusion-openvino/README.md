# FaceFusion OpenVINO for ZSpace

This branch builds a pinned FaceFusion image for the Intel iGPU in the ZSpace Z4 Pro+.

## Pinned stack

- FaceFusion: 3.9.1
- Base runtime: `openvino/ubuntu24_runtime:2025.4.1`
- ONNX Runtime OpenVINO: 1.24.1 (installed by FaceFusion's `install.py openvino`)
- Target device: Intel GPU through `/dev/dri/renderD128`
- ZSpace render group GID observed during probe: `992`

The image intentionally refuses to start FaceFusion if the OpenVINO GPU provider fails and ONNX Runtime falls back to CPU.

## Image

```
ghcr.io/lihan11b/facefusion-openvino:3.9.1-ov2025.4.1
```

The workflow also publishes `latest`, but the fixed tag above is recommended for the NAS.

## ZSpace directories

Create these folders before starting the Compose project:

```
/tmp/zfsv3/sata1/13488873316/data/Docker/FaceFusion/assets
/tmp/zfsv3/sata1/13488873316/data/Docker/FaceFusion/caches
/tmp/zfsv3/sata1/13488873316/data/Docker/FaceFusion/jobs
/tmp/zfsv3/sata1/13488873316/data/Docker/FaceFusion/input
/tmp/zfsv3/sata1/13488873316/data/Docker/FaceFusion/output
/tmp/zfsv3/sata1/13488873316/data/Docker/FaceFusion/temp
```

Use `compose.zspace.yml` in the ZSpace Docker Compose UI.

Do not enable SSH, privileged mode, or "all folders maximum permissions". The GPU is passed only as the render node:

```yaml
devices:
  - /dev/dri/renderD128:/dev/dri/renderD128
group_add:
  - "992"
```

## Startup verification

A healthy start must contain:

```
OpenVINO devices: ['CPU', 'GPU']
Session providers: ['OpenVINOExecutionProvider', 'CPUExecutionProvider']
Inference verification: PASS
GPU PREFLIGHT: PASS
OpenVINOExecutionProvider is active on Intel GPU.
```

If the OpenVINO EP cannot initialize on GPU, the container exits instead of silently running FaceFusion on CPU.

## UI

After the container starts:

```
http://<ZSPACE-IP>:7865
```

The initial execution thread count is 2. After basic stability is confirmed, benchmark 1, 2 and 4 threads rather than assuming a higher value is faster on the N355 iGPU.
