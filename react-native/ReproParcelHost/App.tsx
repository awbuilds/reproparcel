import React, { useEffect, useState } from 'react';
import { Button, SafeAreaView, StatusBar, Text, View } from 'react-native';
import {
  deleteLocalReports,
  initialize,
  recordBreadcrumb,
  setEnabled,
  showReporter,
} from 'reproparcel-react-native';

function App(): React.JSX.Element {
  const [status, setStatus] = useState('Initializing…');

  useEffect(() => {
    initialize({
      enabled: false,
      packageName: 'com.reproparcelhost',
      versionName: '1.0.0',
      versionCode: 1,
      variant: 'debug',
      gitCommit: null,
      capturePolicy: {
        policyVersion: 'rn-host-policy-v1',
        screenshotEnabled: false,
        semanticTextEnabled: false,
        eligibleSurfaces: ['com.reproparcelhost.MainActivity'],
      },
    })
      .then(ok => setStatus(ok ? 'Initialized disabled' : 'Initialization rejected'))
      .catch(error => setStatus(`Initialization failed: ${String(error)}`));
  }, []);

  return (
    <SafeAreaView style={{ flex: 1 }}>
      <StatusBar barStyle="dark-content" />
      <View style={{ padding: 24, gap: 12 }}>
        <Text style={{ fontSize: 22, fontWeight: '600' }}>ReproParcel RN host</Text>
        <Text>Synthetic integration host. No real report data.</Text>
        <Text>{status}</Text>
        <Button
          title="Enable SDK"
          onPress={async () => {
            const ok = await setEnabled(true);
            setStatus(ok ? 'Enabled' : 'Enable failed');
          }}
        />
        <Button
          title="Record breadcrumb"
          onPress={async () => {
            const ok = await recordBreadcrumb({
              category: 'navigation',
              actionId: 'open_home',
              screenId: 'home',
            });
            setStatus(ok ? 'Breadcrumb recorded' : 'Breadcrumb rejected');
          }}
        />
        <Button
          title="Open reporter"
          onPress={async () => {
            const ok = await showReporter('com.reproparcelhost.MainActivity');
            setStatus(ok ? 'Reporter launch requested' : 'Reporter unavailable');
          }}
        />
        <Button
          title="Delete local reports"
          onPress={async () => {
            const ok = await deleteLocalReports();
            setStatus(ok ? 'Local reports deleted' : 'Deletion failed');
          }}
        />
      </View>
    </SafeAreaView>
  );
}

export default App;
