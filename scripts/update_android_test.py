with open(r'C:\Users\MNA\.gemini\antigravity\scratch\abiz-locker\scripts\Phase18AndroidUpdateIntegrityVerificationTest.java', 'r', encoding='utf-8') as f:
    code = f.read()

helper = '''    private static void pass(String name) {
        System.out.println("[PASS] " + name);
        testsPassed++;
    }

    private static void fail(String name, String reason) {
        System.out.println("[FAIL] " + name + ": " + reason);
        testsFailed++;
    }
'''

code = code.replace('private static int testsFailed = 0;', 'private static int testsFailed = 0;\n\n' + helper)

with open(r'C:\Users\MNA\.gemini\antigravity\scratch\abiz-locker\scripts\Phase18AndroidUpdateIntegrityVerificationTest.java', 'w', encoding='utf-8') as f:
    f.write(code)

print("Updated Phase18AndroidUpdateIntegrityVerificationTest.java with pass helper")