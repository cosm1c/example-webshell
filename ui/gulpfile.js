'use strict';

const gulp = require('gulp'),
  gutil = require('gulp-util'),
  webpack = require('webpack');

gulp.task('default', function (cb) {
  gutil.log('Tasks are:');
  gutil.log('\tclean');
  gutil.log('\twebpack-dev-server');
  gutil.log('\ttest');
  gutil.log('\tpackage');
  cb();
});

gulp.task('clean', function (cb) {
  const rimraf = require("rimraf");

  rimraf('dist', function () {
    rimraf('generated', cb);
  });
});

gulp.task('test', function () {
  const mocha = require('gulp-mocha');

  return gulp.src(['js/**/*Test.js'], {read: false})
    .pipe(mocha({}));
});

gulp.task('package', ['clean'], function (cb) {
  const webpackConfig = require('./webpack.production.config.js');

  webpack(webpackConfig, function (err, stats) {
    if (err) throw err;
    gutil.log("[webpack]", stats.toString({
      // see: https://webpack.github.io/docs/node.js-api.html#stats-tostring
    }));
    cb();
  });
});

gulp.task("webpack-dev-server", ['clean'], function (cb) {
  const WebpackDevServer = require("webpack-dev-server"),
    prodWebpackConfig = Object.create(require("./webpack.config.js"));

  new WebpackDevServer(webpack(prodWebpackConfig), {
    contentBase: "http://localhost:8080",
    quiet: false,
    noInfo: true,
    stats: {
      colors: true
    }
  }).listen(9090, "localhost", function (err) {
    if (err) throw err;
    gutil.log("[webpack-dev-server]", "http://localhost:9090/webpack-dev-server/index.html");
    cb();
  });
});
