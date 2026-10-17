# =====================================================================
# SecureVault Next — app module R8 rules
#
# Per Part 12 (Logging Policy) stack traces must never leak sensitive
# values, and per Part 7 the release build must be free of debug
# artifacts. Keep line numbers for crash triage but strip the source
# file name so obfuscated stack traces cannot be trivially mapped
# without the generated mapping file.
# =====================================================================

-keepattributes LineNumberTable
-renamesourcefileattribute SourceFile

# Hilt-generated components are referenced reflectively by the Hilt
# runtime; module-specific keep rules ship inside each module's own
# consumer-rules.pro (added starting with the Hilt-integrated modules
# in later milestones).
