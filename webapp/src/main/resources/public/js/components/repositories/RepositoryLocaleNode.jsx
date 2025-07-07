import React from 'react';
import PropTypes from 'prop-types';
import createReactClass from 'create-react-class';
import EMPTY_LOCALE from './RepositoryLocalesInput';
import{ OverlayTrigger, Tooltip } from 'react-bootstrap';
import SourceLocaleDropdown from "./SourceLocaleDropdown";

const cloneLocale = (locale) => {
    return JSON.parse(JSON.stringify(locale));
}

const RepositoryLocaleNode = createReactClass({
    propTypes: {
        localeObj: PropTypes.object.isRequired,
        onChange: PropTypes.func.isRequired,
        onRemove: PropTypes.func.isRequired,
        isRoot: PropTypes.bool
    },

    handleToBeFullyTranslatedChange(e) {
        const newLocale = cloneLocale(this.props.localeObj);
        newLocale.toBeFullyTranslated = e.target.checked;
        this.props.onChange(newLocale);
    },

    handleAddParentLocale() {
        const newLocale = cloneLocale(this.props.localeObj);
        newLocale.parentLocale = { ...EMPTY_LOCALE };
        this.props.onChange(newLocale);
    },

    handleParentChange(newParent) {
        const newLocale = cloneLocale(this.props.localeObj);
        newLocale.parentLocale = newParent;
        this.props.onChange(newLocale);
    },

    handleRemove() {
        this.props.onRemove();
    },

    handleRemoveParent() {
        const newLocale = cloneLocale(this.props.localeObj);
        delete newLocale.parentLocale;
        this.props.onChange(newLocale);
    },

    handleLocaleSelect(locale) {
        const newLocale = cloneLocale(this.props.localeObj);
        newLocale.locale = locale;
        this.props.onChange(newLocale);
    },

    render() {
        const { localeObj, isRoot } = this.props;
        return (
            <div>
                <div className="repo-locale-row">
                    <OverlayTrigger
                        placement="top"
                        overlay={<Tooltip id="ft-tooltip">To Be Fully Translated</Tooltip>}
                    >
                        <input
                            type="checkbox"
                            checked={!!localeObj.toBeFullyTranslated}
                            onChange={this.handleToBeFullyTranslatedChange}
                            className="repo-locale-checkbox"
                        />
                    </OverlayTrigger>
                    <SourceLocaleDropdown
                        selectedLocale={localeObj.locale || {}}
                        onSelect={this.handleLocaleSelect}
                        className="repo-locale-dropdown"
                    />
                    {localeObj.parentLocale ? null : (
                        <button type="button" onClick={this.handleAddParentLocale} className="repo-locale-add-parent btn btn-secondary">Add Parent</button>
                    )}
                    <button type="button" onClick={this.handleRemove} className="repo-locale-remove btn btn-danger">Remove</button>
                </div>
                {localeObj.parentLocale && (
                    <RepositoryLocaleNode
                        localeObj={localeObj.parentLocale}
                        onChange={this.handleParentChange}
                        onRemove={this.handleRemoveParent}
                        isRoot={false}
                    />
                )}
            </div>
        );
    }
});

export default RepositoryLocaleNode;