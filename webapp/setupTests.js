import '@testing-library/jest-dom/extend-expect';

global.APP_CONFIG = {
  contextPath: '',
  locale: 'en',
  deleteLanguageRoot: '/:projectId',
  repositoriesPageSize: 10,
  appName: 'Mojito',
  user: {
    role: 'ROLE_ADMIN',
    username: 'testuser',
    authorities: ['ROLE_ADMIN']
  }
};
