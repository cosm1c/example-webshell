'use strict';

const path = require('path'),
  webpack = require('webpack'),
  HtmlWebpackPlugin = require('html-webpack-plugin');

module.exports = {
  entry: './app/main.ts',

  output: {
    path: path.join(__dirname, "dist", "ui"),
    filename: "[name].js",
    publicPath: "http://localhost:9090/"
  },

  devtool: "source-map", //'eval-source-map'

  resolve: {
    extensions: ["", ".ts", ".js"]
  },

  plugins: [
    new HtmlWebpackPlugin({
      template: 'index.html',
      inject: true
    }),
    new webpack.DefinePlugin({
      'ENV': JSON.stringify('development')
    })
  ],

  module: {
    loaders: [
      {test: /\.ts$/, loader: "ts-loader"},
      {test: /\.proto$/, loader: "proto-loader"}
    ],

    preLoaders: [
      {test: /\.js$/, loader: "source-map-loader"}
    ]
  }
};
