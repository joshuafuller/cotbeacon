# Cursor On Target Beacon

## Preface
Note that this is a pseudo-fork of [CoT Generator](https://github.com/jonapoul/cotgenerator) with the "Fake Icons" functionality disabled.

## Quick Start
1. Download the installer APK from [the GitHub releases page](https://github.com/jonapoul/cotbeacon/releases), then copy the file to your device and open it in a file browser to install.
2. Open the app and grant permissions for GPS access.
3. Configure as required (see below for more explanation).
4. Tap the green "start" icon in the right hand side of the upper toolbar. This begins the configured packet transmissions.
5. When finished, tap the red "stop" icon on the toolbar, or the "STOP" button on the service notification.

## Operation
This app reads the device's GPS location and translates that to Cursor on Target, then periodically transmits it to the specified destination over either TCP or UDP.

GPS Beacon mode also includes the ability to send "911 Emergency" packets located on your current location. This is found by tapping the red Floating Action Button in the bottom right, then selecting "START EMERGENCY". These are single-transmission events, so they are not periodically re-transmitted unless you explicitly select the option multiple times. When finished, select the "CANCEL EMERGENCY" option to remove the emergency icon from your team's TAK map screen.

Note that there is a slight difference in behaviour between TAK Server and FreeTakServer (FTS) in this cancelling functionality, as of FTS v0.7:
* When TAK Server receives a cancel request, it halts the retransmission of the existing emergency and also removes the icon from the team's TAK map. This removal is almost instantaneous from the point of receiving the cancel request.
* FTS also halts the retransmission, but the "inert" emergency icon still remains on the map screen until it is manually cleared/deleted. FTS also takes up to 10 seconds to deactivate the emergency icon. This deactivation is shown when the icon stops flashing on the map screen, but is still visible.

## Troubleshooting
Note that almost 100% of testing has been done on a OnePlus 6 running Android 10. If there are any compatibility issues or crashes (I'm sure there are), please raise an issue!