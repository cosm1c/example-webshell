'use strict';

var path = require('path'),
  webpack = require('webpack'),
  HtmlWebpackPlugin = require('html-webpack-plugin'),
  LessPluginCleanCSS = require('less-plugin-clean-css'),
  ExtractTextPlugin = require("extract-text-webpack-plugin"),
  StatsPlugin = require('stats-webpack-plugin'),
  failPlugin = require('webpack-fail-plugin');

module.exports = {
  entry: './app/main.ts',

  output: {
    path: path.join(__dirname, "dist", "ui"),
    filename: '[name]-[hash].min.js',
    chunkFilename: "[id]-[hash].chunk.min.js",
    publicPath: '/'
  },

  plugins: [
    failPlugin,
    new webpack.NoErrorsPlugin(),
    new webpack.optimize.OccurenceOrderPlugin(),
    new HtmlWebpackPlugin({
      template: 'index.html',
      inject: true,
      minify: {
        collapseInlineTagWhitespace: false,
        collapseWhitespace: true,
        // conservativeCollapse: true,
        removeComments: true,
        minifyCSS: true,
        minifyJS: true,
        sortAttributes: true,
        sortClassName: true
      }
    }),
    new ExtractTextPlugin('[name]-[hash].min.css'),
    new webpack.optimize.UglifyJsPlugin({
      compressor: {
        warnings: false,
        screw_ie8: true/*,
         lint: true,
         'if_return': true,
         'join_vars': true,
         cascade: true,
         'dead_code': true,
         conditionals: true,
         booleans: true,
         loops: true,
         unused: true*/
      }
    }),
    new StatsPlugin('webpack.stats.json', {
      source: false,
      modules: false
    }),
    new webpack.DefinePlugin({
      'ENV': JSON.stringify('production')
    })
  ],

  resolve: {
    extensions: ["", ".ts", ".js"]
  },

  module: {
    loaders: [
      {test: /\.ts$/, loader: "ts-loader"}
    ]
  },

  lessLoader: {
    lessPlugins: [
      new LessPluginCleanCSS({advanced: true})
    ]
  }
};
