const path = require('path')
const pkg = require('../packages/react-native-nitro-lunar-range-picker/package.json')

/**
 * @type {import('@react-native-community/cli-types').Config}
 */
module.exports = {
    project: {
        ios: {
            automaticPodsInstallation: true,
        },
    },
    dependencies: {
        [pkg.name]: {
            root: path.join(__dirname, '../packages/react-native-nitro-lunar-range-picker'),
        },
    },
}
