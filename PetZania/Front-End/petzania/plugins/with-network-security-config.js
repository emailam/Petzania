const { withAndroidManifest, withDangerousMod } = require('@expo/config-plugins');
const path = require('path');
const fs = require('fs');

const withNetworkSecurityConfig = (config) => {
  return withDangerousMod(config, [
    'android',
    async (config) => {
      const networkSecurityConfigPath = path.join(
        config.modRequest.platformProjectRoot,
        'app/src/main/res/xml/network_security_config.xml'
      );

      // Ensure the xml directory exists
      const xmlDir = path.dirname(networkSecurityConfigPath);
      if (!fs.existsSync(xmlDir)) {
        fs.mkdirSync(xmlDir, { recursive: true });
      }

      // Create the network security config file
      const networkSecurityConfig = `<?xml version="1.0" encoding="utf-8"?>
<network-security-config>
  <domain-config cleartextTrafficPermitted="true">
    <domain includeSubdomains="true">157.230.114.107</domain>
  </domain-config>
</network-security-config>`;

      fs.writeFileSync(networkSecurityConfigPath, networkSecurityConfig);

      return config;
    },
  ]);
};

const withNetworkSecurityConfigManifest = (config) => {
  return withAndroidManifest(config, (config) => {
    const { manifest } = config.modResults;

    // Add networkSecurityConfig to application
    if (manifest.application && manifest.application[0]) {
      manifest.application[0].$['android:networkSecurityConfig'] = '@xml/network_security_config';
      manifest.application[0].$['android:usesCleartextTraffic'] = 'true';
    }

    return config;
  });
};

module.exports = (config) => {
  config = withNetworkSecurityConfig(config);
  config = withNetworkSecurityConfigManifest(config);
  return config;
};
