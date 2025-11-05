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

      // Enhanced network security config to allow cleartext for specific domains and IPs
      const networkSecurityConfig = `<?xml version="1.0" encoding="utf-8"?>
<network-security-config>
  <base-config cleartextTrafficPermitted="true">
    <trust-anchors>
      <certificates src="system"/>
    </trust-anchors>
  </base-config>
  
  <!-- Allow cleartext traffic for development IPs -->
  <domain-config cleartextTrafficPermitted="true">
    <domain includeSubdomains="true">192.168.1.3</domain>
    <domain includeSubdomains="true">10.0.2.2</domain>
    <domain includeSubdomains="true">localhost</domain>
    <domain includeSubdomains="true">127.0.0.1</domain>
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

    if (manifest.application && manifest.application[0]) {
      // Link the security config and enable cleartext traffic
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
