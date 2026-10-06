# ChickenRice Translate-only CPU image

This branch only contains the Docker build assets for the ChickenRice Japanese-to-Chinese CPU image used on ZSpace/NAS.

- Upstream ChickenRice commit: `62104884d8553be1c30bc88c157d82b2b8d0792b`
- Target: `linux/amd64`
- Runtime: Python 3.10, CTranslate2 CPU, ONNX Runtime CPU
- Compute type: `int8`
- Model set: voice-optimized VAD + whisper-base feature extractor + 海南鸡 v2 5000小时 translate model
- Model files are downloaded automatically to the persistent `/app/models` mount on first container start.

Published image:

```text
ghcr.io/lihan11b/chickenrice-translate-cpu:62104884-cpu-v1
```

Use `chickenrice-docker/compose.zspace.yml` as the ZSpace template and replace the three host-side `source:` paths.
