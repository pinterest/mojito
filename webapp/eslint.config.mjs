import globals from "globals";
import esLintPluginReact from "eslint-plugin-react";
import esLintPluginReactHooks from "eslint-plugin-react-hooks";

export default [
  {
    languageOptions: {
        ecmaVersion: 2021,
        sourceType: "module",
        globals: {
            ...globals.browser,
            ...globals.node,
        }
    },
    plugins: {
      react: esLintPluginReact,
      reactHooks: esLintPluginReactHooks, 
    },
    rules: {
      'react/prop-types': 'off',
    },
    settings: {
      react: {
        version: 'detect', // Automatically detect the React version
      },
    },
  }
];