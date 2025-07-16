import path from 'path';
import webpack from 'webpack';
import TerserPlugin from 'terser-webpack-plugin';
import HtmlWebpackPlugin from 'html-webpack-plugin';
import { fileURLToPath } from 'url';
import { dirname } from 'path';

// Polyfill __dirname for ESM
const __filename = fileURLToPath(import.meta.url);
const __dirname = dirname(__filename);

export default (env) => {
    env = env || {};

    const isProdEnv = Boolean(env.production);
    const inlineSourceMap = Boolean(env.inlineSourceMap);

    const config = {
        entry: {
            'app': path.resolve(__dirname, './src/main/resources/public/js/app.jsx'),
            'css': path.resolve(__dirname, './src/main/resources/sass/mojito.scss')
        },
        output: {
            path: path.resolve(__dirname, './target/classes/public'),
            publicPath: '/',
            filename: 'js/[name]-[contenthash].js',
            chunkFilename: 'js/[name]-[chunkhash].js'
        },
        mode: isProdEnv ? 'production' : 'development',
        devtool: inlineSourceMap ? "inline-source-map" : false,
        module: {
            rules: [
                {
                    test: /\.jsx?$/,
                    exclude: /node_modules/,
                    use: {
                        loader: 'babel-loader',
                        options: {
                            presets: [
                                '@babel/preset-env',
                                '@babel/preset-react'
                            ],
                            plugins: [
                                '@babel/plugin-proposal-function-bind',
                                '@babel/plugin-proposal-export-default-from',
                                '@babel/plugin-proposal-logical-assignment-operators',
                                ['@babel/plugin-proposal-optional-chaining', { loose: false }],
                                ['@babel/plugin-proposal-pipeline-operator', { proposal: 'minimal' }],
                                ['@babel/plugin-proposal-nullish-coalescing-operator', { loose: false }],
                                '@babel/plugin-proposal-do-expressions',
                                ['@babel/plugin-proposal-decorators', { legacy: true }],
                                '@babel/plugin-proposal-function-sent',
                                '@babel/plugin-proposal-export-namespace-from',
                                '@babel/plugin-proposal-numeric-separator',
                                '@babel/plugin-proposal-throw-expressions',
                                '@babel/plugin-syntax-dynamic-import',
                                '@babel/plugin-syntax-import-meta',
                                ['@babel/plugin-proposal-class-properties', { loose: false }],
                                '@babel/plugin-proposal-json-strings'
                            ]
                        }
                    }
                },
                {
                    test: /\.(svg|png)$/,
                    type: 'asset/resource',
                },
                {
                    test: /\.(gif|jpe?g)$/i,
                    use: [
                        {
                            loader: 'file-loader',
                            options: {
                                name: 'img/[name]-[contenthash].[ext]',
                            }
                        },
                        {
                            loader: 'image-webpack-loader',
                            options: {
                                name: 'img/[name]-[contenthash].[ext]',
                                query: {
                                    mozjpeg: {
                                        progressive: true
                                    },
                                    gifsicle: {
                                        interlaced: false
                                    },
                                    optipng: {
                                        optimizationLevel: 4
                                    },
                                    pngquant: {
                                        quality: '75-90',
                                        speed: 3
                                    },
                                },
                            }
                        }
                    ]
                },
                {
                    test: /\.properties$/,
                    exclude: /node_modules/,
                    use: [
                        {
                            loader: path.resolve('src/main/webpackloader/properties.js')
                        }
                    ],
                },
                {
                    // __webpack_public_path__ is not supported by ExtractTextPlugin
                    // so we inline all the fonts here. If not inlined, references
                    // to the font are invalid if mojito is deployed with a
                    // specific deploy path.
                    // hardcoded for deploy path for test -->
                    //    name: '{deployPath}/fonts/[name]-[contenthash].[ext]'

                    test: /\.(eot|ttf|woff|woff2)$/,
                    loader: 'url-loader',
                    options: {
                        name: 'fonts/[name]-[contenthash].[ext]'
                    }
                },

                {
                    test: /\.scss$/,
                    use: [{
                            loader: "style-loader" // creates style nodes from JS strings
                        }, {
                            loader: "css-loader" // translates CSS into CommonJS
                        }, {
                            loader: "sass-loader",
                            options: {
                                sassOptions: {
                                    precision: 8
                                },
                                sourceMap: true
                            }
                        }]
                },
            ]
        },
        optimization: {
            minimizer: [new TerserPlugin()],
            minimize: isProdEnv,
        },
        performance: {
            hints: 'warning',
            maxEntrypointSize: 1_800_000, // 1.8MB
            maxAssetSize: 1_800_000 // 1.8MB
        },
        plugins: [],
        resolve: {
            extensions: ['.js', '.jsx']
        }
    };
    
    config.plugins.push(new HtmlWebpackPlugin({
        filename: path.resolve(__dirname, './target/classes/templates/index.html'),
        template: 'src/main/resources/templates/index.html',
        favicon: 'src/main/resources/favicon.ico',
        inject: false
    }));

    if (isProdEnv) {
        config.plugins.push(
                new webpack.DefinePlugin({
                    'process.env': {
                        'NODE_ENV': JSON.stringify('production')
                    }
                }));
    }

    return config;
};