import React from "react";
import PropTypes from "prop-types";
import Locales from "../../utils/Locales";

class RepositoryLocaleDropdown extends React.Component {
    static propTypes = {
        onSelect: PropTypes.func.isRequired,
        selectedLocale: PropTypes.object.isRequired,
        localeOptions: PropTypes.arrayOf(
            PropTypes.shape({
                bcp47Tag: PropTypes.string,
                id: PropTypes.number
            })
        ).isRequired,
        defaultLocaleTag: PropTypes.string
    };

    constructor(props) {
        super(props);
        this.state = {
            searchTerm: "",
            open: false,
        };
        this.dropdownRef = React.createRef();
    }

    componentDidMount() {
        document.addEventListener("mousedown", this.handleClickOutside);
        if (this.props.defaultLocaleTag) {
            const defaultLocale = this.getDefaultLocale(this.props.defaultLocaleTag);
            if (defaultLocale) {
                this.handleSelect(defaultLocale);
            }
        }
    }

    getDefaultLocale = (defaultLocaleTag) => {
        return this.props.localeOptions.find(locale => locale.bcp47Tag === defaultLocaleTag) || null;
    }

    componentWillUnmount() {
        document.removeEventListener("mousedown", this.handleClickOutside);
    }

    handleSelect = (locale) => {
        const display = `${Locales.getDisplayName(locale.bcp47Tag)} ${locale.bcp47Tag}`;
        this.setState({ open: false, searchTerm: display });
        if (this.props.onSelect) {
            this.props.onSelect(locale);
        }
    };

    handleInputChange = (e) => {
        this.setState({ searchTerm: e.target.value, open: true });
    };

    handleInputClick = () => {
        this.setState({ open: true });
    };

    handleClickOutside = (event) => {
        if (
            this.dropdownRef.current &&
            !this.dropdownRef.current.contains(event.target)
        ) {
            this.setState(prevState => ({
                open: false,
                searchTerm: prevState.searchTerm === "" && this.props.selectedLocale.bcp47Tag
                    ? `${Locales.getDisplayName(this.props.selectedLocale.bcp47Tag)} ${this.props.selectedLocale.bcp47Tag}`
                    : prevState.searchTerm
            }));
        }
    };

    getInputValue = () => {
        if (this.state.open) {
            return this.state.searchTerm;
        }
        if (this.props.selectedLocale && this.props.selectedLocale.bcp47Tag) {
            return `${Locales.getDisplayName(this.props.selectedLocale.bcp47Tag)} ${this.props.selectedLocale.bcp47Tag}`;
        }
        if (this.props.defaultLocale) {
            return `${Locales.getDisplayName(this.props.defaultLocale)} ${this.props.defaultLocale}`;
        }
        return "";
    };

    getFilteredLocales = () => {
        return this.props.localeOptions.filter(locale => {
            const displayString = `${Locales.getDisplayName(locale.bcp47Tag)} ${locale.bcp47Tag}`.toLowerCase();
            return displayString.includes(this.state.searchTerm.toLowerCase());
        });
    };

    render() {
        const filteredLocales = this.getFilteredLocales();
        return (
            <div ref={this.dropdownRef} className="locale-dropdown-root">
                <input
                    type="text"
                    className="form-control locale-dropdown-input"
                    placeholder="Choose a locale"
                    value={this.getInputValue()}
                    onChange={this.handleInputChange}
                    onClick={this.handleInputClick}
                    autoComplete="off"
                />
                {this.state.open && (
                    <ul
                        className="dropdown-menu locale-dropdown-menu"
                    >
                        {filteredLocales.length > 0 ? (
                            filteredLocales.map(locale => (
                                <li
                                    key={locale.id}
                                    className="locale-dropdown-item"
                                    onClick={() => this.handleSelect(locale)}
                                >
                                    <a tabIndex="-1">
                                        {Locales.getDisplayName(locale.bcp47Tag)} {locale.bcp47Tag}
                                    </a>
                                </li>
                            ))
                        ) : (
                            <li className="disabled locale-dropdown-item-disabled">
                                <a tabIndex="-1">No results</a>
                            </li>
                        )}
                    </ul>
                )}
            </div>
        );
    }
}

export default RepositoryLocaleDropdown;
