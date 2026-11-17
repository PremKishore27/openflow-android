# Third-party notices

OpenFlow itself is MIT licensed (see [LICENSE](LICENSE)). On Android it uses:

- **sherpa-onnx** (Apache License 2.0), https://github.com/k2-fsa/sherpa-onnx. Runs speech models on the phone.
  Downloaded by `scripts/fetch-sherpa-onnx.sh` at build time. It is not stored in this repository. It bundles
  **ONNX Runtime** (MIT).
- **Whisper** speech models (MIT), by OpenAI, converted for sherpa-onnx by its maintainers. The app downloads the
  English "small" or "base" model from Hugging Face the first time you choose it. The models are not stored in
  this repository.
