module.exports = {
  extends: '@curiostack/base',
  parserOptions: {
    tsconfigRootDir: __dirname,
  },
  rules: {
    '@typescript-eslint/no-unused-vars': 'off',
    'import/prefer-default-export': 'off',
  },
};
