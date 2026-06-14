import React from "react";
import PropTypes from "prop-types";
import {injectIntl, FormattedMessage} from 'react-intl';
import Locales from "../../utils/Locales";

class SearchableLocaleDropdown extends React.Component {
    static propTypes = {
        onSelect: PropTypes.func.isRequired,
        selectedLocaleId: PropTypes.oneOfType([PropTypes.string, PropTypes.number]),
        localeOptions: PropTypes.arrayOf(
            PropTypes.shape({
                bcp47Tag: PropTypes.string,
                id: PropTypes.number
            })
        ).isRequired,
    };

    constructor(props) {
        super(props);
        this.state = {
            searchTerm: "",
            isOpen: false,
        };
        this.dropdownRef = React.createRef();
        this._filteredCache = { localeOptions: null, searchTerm: null, result: [] };
    }

    componentDidMount() {
        document.addEventListener("mousedown", this.handleClickOutside);
    }

    componentWillUnmount() {
        document.removeEventListener("mousedown", this.handleClickOutside);
    }

    componentDidUpdate(prevProps) {
        if (prevProps.selectedLocaleId !== this.props.selectedLocaleId) {
            if (!this.props.selectedLocaleId) {
                this.setState({ searchTerm: "" });
            }
        }
    }

    getLocaleDisplayName = (bcp47Tag) => {
        return `${Locales.getDisplayName(bcp47Tag)} (${bcp47Tag})`;
    };

    getSelectedLocale = () => {
        if (!this.props.selectedLocaleId) return null;
        return this.props.localeOptions.find(
            l => l.id === Number(this.props.selectedLocaleId)
        ) || null;
    };

    handleSelect = (locale) => {
        this.setState({ isOpen: false, searchTerm: locale ? this.getLocaleDisplayName(locale.bcp47Tag) : "" });
        this.props.onSelect(locale);
    };

    handleInputChange = (e) => {
        this.setState({ searchTerm: e.target.value, isOpen: true });
    };

    handleInputClick = () => {
        this.setState({ isOpen: true });
    };

    handleLocaleClick = (e) => {
        const localeId = Number(e.currentTarget.dataset.id);
        const locale = this.props.localeOptions.find(l => l.id === localeId);
        if (locale) {
            this.handleSelect(locale);
        }
    };

    handleClickOutside = (event) => {
        if (
            this.dropdownRef.current &&
            !this.dropdownRef.current.contains(event.target)
        ) {
            const selectedLocale = this.getSelectedLocale();
            this.setState({
                isOpen: false,
                searchTerm: selectedLocale ? this.getLocaleDisplayName(selectedLocale.bcp47Tag) : ""
            });
        }
    };

    getInputValue = () => {
        if (this.state.isOpen) {
            return this.state.searchTerm;
        }
        const selectedLocale = this.getSelectedLocale();
        if (selectedLocale) {
            return this.getLocaleDisplayName(selectedLocale.bcp47Tag);
        }
        return "";
    };

    getFilteredLocales = () => {
        const { localeOptions } = this.props;
        const { searchTerm } = this.state;
        if (this._filteredCache.localeOptions === localeOptions && this._filteredCache.searchTerm === searchTerm) {
            return this._filteredCache.result;
        }
        const result = localeOptions.filter(locale => {
            return this.getLocaleDisplayName(locale.bcp47Tag)
                .toLowerCase()
                .includes(searchTerm.toLowerCase());
        });
        this._filteredCache = { localeOptions, searchTerm, result };
        return result;
    };

    render() {
        const filteredLocales = this.getFilteredLocales();
        return (
            <div ref={this.dropdownRef} className="modal-dropdown-root">
                <input
                    type="text"
                    className="form-control searchable-dropdown-input"
                    placeholder={this.props.intl.formatMessage({id: "rewriteRules.localeDropdown.placeholder"})}
                    value={this.getInputValue()}
                    onChange={this.handleInputChange}
                    onClick={this.handleInputClick}
                    autoComplete="off"
                />
                {this.state.isOpen && (
                    <ul className="dropdown-menu searchable-dropdown-menu">
                        {filteredLocales.length > 0 ? (
                            filteredLocales.map(locale => (
                                <li
                                    key={locale.id}
                                    className="searchable-dropdown-item"
                                    data-id={locale.id}
                                    onClick={this.handleLocaleClick}
                                >
                                    <a>
                                        {this.getLocaleDisplayName(locale.bcp47Tag)}
                                    </a>
                                </li>
                            ))
                        ) : (
                            <li className="disabled searchable-dropdown-item-disabled">
                                <a><FormattedMessage id="rewriteRules.localeDropdown.noResults"/></a>
                            </li>
                        )}
                    </ul>
                )}
            </div>
        );
    }
}

export default injectIntl(SearchableLocaleDropdown);
