# Seriendruck

Diese Extension wird nicht mehr gewartet.

## Extension bauen
Die LibreOffice-Extension lässt sich ganz einfach mittels `mvn clean package` bauen.
Die Extension liegt dann unter `mailmerge/target/MailMerge.oxt`

## Debian Paket bauen
Zuerst muss die Extension gebaut werden. Danach kann mittels `sbuild` das Debian-Paket erstellt werden.
