const path = require('path');
const webpack = require('webpack');

const HtmlWebpackPlugin = require('html-webpack-plugin');
const HtmlWebpackInlineSourcePlugin = require('html-webpack-inline-source-plugin');

module.exports = {
    mode: 'development',
    entry: './src/index.js',

    output: {
        filename: '[name].[hash].js',
        path: path.resolve(__dirname, 'dist')
    },

    plugins: [
        new webpack.ProgressPlugin(),
        new HtmlWebpackPlugin({
            template: 'src/index.html',
            inlineSource: '.(js|css)$'
        }),
        new HtmlWebpackInlineSourcePlugin()
    ],

    module: {
        rules: [
            {
                test: /.(js|jsx)$/,
                include: [path.resolve(__dirname, 'src')],
                loader: 'babel-loader',
                options: {
                    plugins: ['syntax-dynamic-import', '@babel/plugin-proposal-class-properties'],
                    presets: ['@babel/preset-env', '@babel/preset-react'],
                }
            },
            {
                test: /\.css$/,
                use: [
                    'style-loader',
                    'css-loader'
                ],
            },
        ]
    },

    devServer: {
        open: true,
        hot: true
    }
};

