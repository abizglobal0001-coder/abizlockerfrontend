with open(r'C:\Users\MNA\.gemini\antigravity\scratch\abiz-locker\scripts\Phase18AndroidUpdateIntegrityVerificationTest.java', 'r', encoding='utf-8') as f:
    code = f.read()

target = 'String prefsXml = execAdb(\"shell\", \"cat\", \"/data/data/\" + APP_PKG + \"/shared_prefs/abiz_local_state.xml\");'
replacement = 'String prefsXml = execAdb(\"shell\", \"run-as\", APP_PKG, \"cat\", \"/data/data/\" + APP_PKG + \"/shared_prefs/abiz_local_state.xml\");'

code = code.replace(target, replacement)

with open(r'C:\Users\MNA\.gemini\antigravity\scratch\abiz-locker\scripts\Phase18AndroidUpdateIntegrityVerificationTest.java', 'w', encoding='utf-8') as f:
    f.write(code)

print("Updated Phase18AndroidUpdateIntegrityVerificationTest with run-as")