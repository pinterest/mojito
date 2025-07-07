import React from "react";
import PropTypes from "prop-types";
import createReactClass from "create-react-class";
import RepositoryLocaleNode from "./RepositoryLocaleNode";

export const EMPTY_LOCALE = {
    locale: {},
    toBeFullyTranslated: false
};

const RepositoryLocalesInput = createReactClass({
    displayName: "RepositoryLocalesInput",
    propTypes: {
        repositoryLocales: PropTypes.arrayOf(
            PropTypes.shape({
                locale: PropTypes.object,
                parentLocale: PropTypes.object,
                toBeFullyTranslated: PropTypes.bool
            })
        ),
        onRepositoryLocalesChange: PropTypes.func.isRequired,
    },

    getDefaultProps() {
        return {
            repositoryLocales: []
        };
    },

    handleAddLocale() {
        const newLocales = [...this.props.repositoryLocales, { ...EMPTY_LOCALE }];
        this.props.onRepositoryLocalesChange(newLocales);
    },

    handleLocaleChange(idx, newLocale) {
        const newLocales = this.props.repositoryLocales.map((l, i) => i === idx ? newLocale : l);
        this.props.onRepositoryLocalesChange(newLocales);
    },

    handleRemoveLocale(idx) {
        const newLocales = this.props.repositoryLocales.filter((_, i) => i !== idx);
        this.props.onRepositoryLocalesChange(newLocales);
    },

    render() {
        const { repositoryLocales } = this.props;
        return (
            <div>
                <label className="mbm">Repository Locales*</label>
                <div>
                    {repositoryLocales.length === 0 && (
                        <div className="mbm">
                            No locales added yet. Click "Add Locale" to start.
                        </div>
                    )}
                    {repositoryLocales.map((locale, idx) => (
                        <RepositoryLocaleNode
                            key={idx}
                            localeObj={locale}
                            onChange={newLocale => this.handleLocaleChange(idx, newLocale)}
                            onRemove={() => this.handleRemoveLocale(idx)}
                            isRoot={true}
                        />
                    ))}
                    <button type="button" onClick={this.handleAddLocale} className="btn btn-default">Add Locale</button>
                </div>
            </div>
        );
    }
});

export default RepositoryLocalesInput;
