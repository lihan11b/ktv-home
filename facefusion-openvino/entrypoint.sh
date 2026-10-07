#!/usr/bin/env bash
set -euo pipefail

if [[ -f /opt/intel/openvino/setupvars.sh ]]; then
  # Required so the OpenVINO 2025.4 runtime and GPU plugin from the base image
  # are visible to ONNX Runtime OpenVINO EP.
  source /opt/intel/openvino/setupvars.sh >/dev/null
fi

run_preflight() {
  python /usr/local/lib/facefusion-gpu-preflight.py
}

case "${1:-run}" in
  run)
    shift || true
    run_preflight
    exec python facefusion.py run \
      --execution-device-ids 0 \
      --execution-providers openvino \
      --execution-thread-count "${FACEFUSION_EXECUTION_THREAD_COUNT:-2}" \
      --temp-path "${FACEFUSION_TEMP_PATH:-/temp}" \
      --jobs-path "${FACEFUSION_JOBS_PATH:-/facefusion/.jobs}" \
      "$@"
    ;;
  doctor)
    run_preflight
    echo "FaceFusion version:"
    python facefusion.py --version
    ;;
  bash)
    exec /bin/bash
    ;;
  *)
    exec "$@"
    ;;
esac
