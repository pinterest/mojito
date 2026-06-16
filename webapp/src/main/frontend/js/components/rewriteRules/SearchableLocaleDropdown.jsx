import React from "react";
import PropTypes from "prop-types";
import {injectIntl, FormattedMessage} from 'react-intl';
import Locales from "../../utils/Locales";
import RepositoryStore from "../../stores/RepositoryStore";

class SearchableLocaleDropdown extends React.Component {
    static propTypes = {
        onSelect: PropTypes.func.isRequired,
        selectedLocaleId: PropTypes.oneOfType([PropTypes.string, PropTypes.number]),
        repositoryId: PropTypes.oneOfType([PropTypes.string, PropTypes.number]),
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
        this._filteredCache = { localeOptions: null, searchTerm: null, repositoryId: null, result: [] };
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
        if (prevProps.repositoryId !== this.props.repositoryId && this.props.repositoryId && this.props.selectedLocaleId) {
            const baseLocales = RepositoryStore.getAllLocalesForRepositoryIds([Number(this.props.repositoryId)]);
            if (!baseLocales.some(locale => locale.id === Number(this.props.selectedLocaleId))) {
                this.setState({ searchTerm: "" });
                this.props.onSelect(null);
            }
        }
    }

    getLocaleDisplayName = (bcp47Tag) => {
        return Locales.getDisplayName(bcp47Tag);
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
        const { localeOptions, repositoryId } = this.props;
        const { searchTerm } = this.state;

        let baseLocales = localeOptions;
        if (repositoryId) {
            baseLocales = RepositoryStore.getAllLocalesForRepositoryIds([Number(repositoryId)]);
        }

        const cacheHit = repositoryId
            ? this._filteredCache.repositoryId === repositoryId && this._filteredCache.searchTerm === searchTerm
            : this._filteredCache.localeOptions === baseLocales && this._filteredCache.searchTerm === searchTerm;
        if (cacheHit) {
            return this._filteredCache.result;
        }
        const term = searchTerm.toLowerCase();
        const result = baseLocales.filter(locale =>
            this.getLocaleDisplayName(locale.bcp47Tag).toLowerCase().includes(term)
        );
        this._filteredCache = { localeOptions: baseLocales, searchTerm, repositoryId, result };
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
