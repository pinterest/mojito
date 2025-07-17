// jest.config.js
export default {
  testEnvironment: 'node',
  transform: {
    '^.+\\.[jt]sx?$': ['babel-jest', { configFile: './.babelrc' }],
  },
  moduleNameMapper: {},
  testPathIgnorePatterns: ['/node/'],
  // If you use any setup files, add them here
  // setupFiles: [],
};
