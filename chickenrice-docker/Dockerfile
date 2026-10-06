FROM python:3.10-slim-bookworm

ARG CHICKENRICE_REPO=https://github.com/TransWithAI/Faster-Whisper-TransWithAI-ChickenRice.git
ARG CHICKENRICE_REF=62104884d8553be1c30bc88c157d82b2b8d0792b

LABEL org.opencontainers.image.source="https://github.com/lihan11b/ktv-home"
LABEL org.opencontainers.image.description="ChickenRice Translate-only CPU image for ZSpace/NAS"
LABEL org.opencontainers.image.licenses="MIT"

ENV DEBIAN_FRONTEND=noninteractive \
    PYTHONUNBUFFERED=1 \
    PYTHONDONTWRITEBYTECODE=1 \
    PIP_DISABLE_PIP_VERSION_CHECK=1 \
    LANG=C.UTF-8 \
    LC_ALL=C.UTF-8

RUN apt-get update && apt-get install -y --no-install-recommends \
      bash ca-certificates ffmpeg git libgomp1 libsndfile1 patch \
    && rm -rf /var/lib/apt/lists/*

RUN python -m pip install --no-cache-dir --upgrade pip \
    && python -m pip install --no-cache-dir \
      "numpy==1.26.4" \
      "ctranslate2>=4.5,<5" \
      "faster-whisper>=1.0,<2" \
      "transformers>=4.30,<5" \
      "onnxruntime==1.23.2" \
      "librosa>=0.10,<1" \
      "pyjson5>=1.6,<2" \
      "MarkupSafe==2.1.5" \
      "requests>=2.28,<3" \
      "setuptools>=65,<82"

RUN git clone --filter=blob:none "${CHICKENRICE_REPO}" /app \
    && cd /app \
    && git checkout "${CHICKENRICE_REF}" \
    && git remote remove origin \
    && FASTER_WHISPER_PATH="$(python -c 'import importlib.util,os; s=importlib.util.find_spec("faster_whisper"); print(os.path.dirname(s.origin))')" \
    && if patch -p1 -d "${FASTER_WHISPER_PATH}/.." < /app/patches/batch-transcribe.patch; then \
         echo "Applied upstream faster-whisper patch"; \
       else \
         echo "WARNING: upstream faster-whisper patch did not apply; continuing as upstream CI does"; \
       fi

COPY chickenrice-translate /usr/local/bin/chickenrice-translate

RUN chmod +x /usr/local/bin/chickenrice-translate \
    && mkdir -p /input /output /app/models

WORKDIR /app
VOLUME ["/input", "/output", "/app/models"]

ENTRYPOINT ["/usr/local/bin/chickenrice-translate"]
CMD ["idle"]
