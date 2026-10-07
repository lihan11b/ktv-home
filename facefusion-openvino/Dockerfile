FROM openvino/ubuntu24_runtime:2025.4.1

USER root

ARG FACEFUSION_VERSION=3.9.1

ENV GRADIO_SERVER_NAME=0.0.0.0 \
    PIP_BREAK_SYSTEM_PACKAGES=1 \
    PYTHONUNBUFFERED=1 \
    FACEFUSION_EXECUTION_THREAD_COUNT=2 \
    FACEFUSION_TEMP_PATH=/temp \
    FACEFUSION_JOBS_PATH=/facefusion/.jobs

WORKDIR /facefusion

RUN apt-get update \
    && apt-get install -y --no-install-recommends \
       ca-certificates \
       curl \
       ffmpeg \
       git \
    && rm -rf /var/lib/apt/lists/*

RUN git clone \
      --branch "${FACEFUSION_VERSION}" \
      --single-branch \
      --depth 1 \
      https://github.com/facefusion/facefusion.git . \
    && python install.py openvino --skip-conda \
    && python -c "import onnxruntime as ort; assert ort.__version__ == '1.24.1', ort.__version__; assert 'OpenVINOExecutionProvider' in ort.get_available_providers(), ort.get_available_providers()" \
    && rm -rf .git

COPY gpu_preflight.py /usr/local/lib/facefusion-gpu-preflight.py
COPY entrypoint.sh /usr/local/bin/facefusion-openvino

RUN chmod 0755 /usr/local/bin/facefusion-openvino \
    && mkdir -p /facefusion/.assets /facefusion/.caches /facefusion/.jobs /input /output /temp

EXPOSE 7860

ENTRYPOINT ["/usr/local/bin/facefusion-openvino"]
CMD ["run"]
