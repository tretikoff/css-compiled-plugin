module.exports = function rules(plugins) {
    return [
        {
            test: /\.css$/,
            include: [
                /[\/\\]plugin.*?[\/\\]static[\/\\]/,
            ],
            use: [
                'style-loader',
                {
                    loader : 'css-loader',
                    options: {
                        esModule: false
                    }
                }
            ]
        },
    ]
};