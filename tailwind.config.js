module.exports = {
  mode: 'jit',
  purge: {
    content: ['./src/cljs/**/*.cljs',
              './test/cljs/**/*.cljs'],
    options: {
      defaultExtractor: content => content.match(/[^<>"'.`\s]*[^<>"'.`\s:]/g)
    }
  },
  variants: {},
  plugins: [
    require('@tailwindcss/forms')
  ],
  theme: {
    fontSize: {
      '2xs': '.5rem',
      'xs': '.75rem',
      'sm': '.875rem',
      'tiny': '.875rem',
      'base': '1rem',
      'lg': '1.125rem',
      'xl': '1.25rem',
      '2xl': '1.5rem',
      '3xl': '1.875rem',
      '4xl': '2.25rem',
      '5xl': '3rem',
      '6xl': '4rem',
      '7xl': '5rem',
    },
    extend: {
      inset: {
        "1/6": "16.7%",
        "1/5": "20%",
        "4/5": "80%",
        "95p": "95%",
        "90p": "90%",
        "110p": "110%",
        "120p": "120%",
        "130p": "130%",
      }
    }
  }
}
