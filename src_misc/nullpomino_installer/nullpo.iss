; Script generated by the Inno Setup Script Wizard.
; SEE THE DOCUMENTATION FOR DETAILS ON CREATING INNO SETUP SCRIPT FILES!

[Setup]
; NOTE: The value of AppId uniquely identifies this application.
; Do not use the same AppId value in installers for other applications.
; (To generate a new GUID, click Tools | Generate GUID inside the IDE.)
AppId={{92421394-0936-4142-B3BD-400D24E8D662}
AppName=NullpoMino_Alt
AppVersion=7.7.2025
;AppVerName=NullpoMino-Alt 7.7.2025
AppPublisher=NullNoname
AppPublisherURL=http://code.google.com/p/nullpomino/
AppSupportURL=http://code.google.com/p/nullpomino/
AppUpdatesURL=http://code.google.com/p/nullpomino/
DefaultDirName={sd}\NullpoMino
DefaultGroupName=NullpoMino
OutputBaseFilename=Nullpomino_7.7.2025
Compression=lzma
SolidCompression=yes
; DisableProgramGroupPage=yes

[Languages]
Name: "english"; MessagesFile: "compiler:Default.isl"
Name: "japanese"; MessagesFile: "compiler:Languages\Japanese.isl"

[Tasks]
Name: "extraicons"; Description: "{cm:CreateDesktopIcon}"; GroupDescription: "{cm:AdditionalIcons}"

[Files]
; Source: "C:\Users\Wojtek\Tymczasowe\npm\NullpoMino.exe"; DestDir: "{app}"; Flags: ignoreversion
Source: "C:\Users\Wojtek\Tymczasowe\npm\*"; DestDir: "{app}"; Flags: ignoreversion recursesubdirs createallsubdirs
; NOTE: Don't use "Flags: ignoreversion" on any shared system files

[Icons]
Name: "{group}\NullpoMino"; Filename: "{app}\NullpoMino.exe"
Name: "{group}\README (English)"; Filename: "{app}\readme_en.txt"; Languages: english
Name: "{group}\README (Japanese)"; Filename: "{app}\readme_jp.txt"; Languages: japanese
Name: "{commondesktop}\NullpoMino"; Filename: "{app}\NullpoMino.exe"; Tasks: extraicons

[Run]
Filename: "{app}\NullpoMino.exe"; Description: "{cm:LaunchProgram,NullpoMino}"; Flags: nowait postinstall skipifsilent
