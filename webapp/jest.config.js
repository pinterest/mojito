export default {
  testEnvironment: 'jsdom',
  transform: {
    '^.+\\.[jt]sx?$': ['babel-jest', { configFile: './.babelrc' }],
  },
  setupFilesAfterEnv: [
    '<rootDir>/setupTests.js'
  ],
  moduleNameMapper: {},
  testPathIgnorePatterns: ['/node/']
};
