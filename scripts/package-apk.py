"""Package a built debug APK without including any local keys or user data."""
import hashlib
import json
import os
import re
import shutil
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
source = ROOT / 'app/build/outputs/apk/debug/app-debug.apk'
metadata_path = source.parent / 'output-metadata.json'
if not source.is_file():
    raise SystemExit('Build the debug APK before packaging it.')
metadata = json.loads(metadata_path.read_text(encoding='utf-8'))['elements'][0]
version = metadata['versionName']
ref = os.environ.get('GITHUB_REF_NAME', f'v{version}')
safe_ref = re.sub(r'[^a-zA-Z0-9._-]', '-', ref)[:100].strip('.-') or 'manual'
output = ROOT / 'ci-output'
output.mkdir(exist_ok=True)
apk = output / f'BodyRecord-{safe_ref}-debug.apk'
shutil.copyfile(source, apk)
digest = hashlib.sha256(apk.read_bytes()).hexdigest()
(output / 'SHA256SUMS.txt').write_text(f'{digest}  {apk.name}\n', encoding='utf-8')
info = {
    'applicationId': 'com.bodyrecord.app',
    'versionName': version,
    'versionCode': metadata['versionCode'],
    'ref': ref,
    'commit': os.environ.get('GITHUB_SHA', ''),
    'buildType': 'debug',
    'signing': 'configured-debug' if (ROOT / '.tools/signing/debug.keystore').is_file() else 'temporary-debug',
    'apk': apk.name,
    'sha256': digest,
}
(output / 'build-info.json').write_text(json.dumps(info, ensure_ascii=False, indent=2)+'\n', encoding='utf-8')
(output / 'RELEASE_NOTES.md').write_text(
    f'# 轻记 {ref}\n\n'
    f'应用版本：{version}（{metadata["versionCode"]}）。构建类型：debug，供个人测试。\n\n'
    'APK、SHA-256 校验文件和构建信息随此版本附带；构建前已运行单元 / 界面测试与 Android Lint。\n\n'
    '未配置 ANDROID_DEBUG_KEYSTORE_BASE64 时使用临时调试签名，无法保证与本地版或其他构建覆盖兼容。'
    '请保留原应用及其数据，配置相同签名后再覆盖升级。\n\n'
    '内置 ExerciseDB 演示素材仅供当前个人、非商业测试，素材权利归原提供者。\n',
    encoding='utf-8',
)
print(f'Packaged {apk.name} ({apk.stat().st_size} bytes)')
