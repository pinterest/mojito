import React from "react";
import PropTypes from "prop-types";
import LocaleActions from "../../actions/LocaleActions";
import LocaleStore from "../../stores/LocaleStore";
import Locales from "../../utils/Locales";

class SourceLocaleDropdown extends React.Component {
    static propTypes = {
        onSelect: PropTypes.func.isRequired,
        selectedLocale: PropTypes.object.isRequired,
    };

    constructor(props) {
        super(props);
        this.state = {
            locales: [],
            searchTerm: "",
            open: false,
        };
        this.dropdownRef = React.createRef();
    }

    componentDidMount() {
        LocaleStore.listen(this.localeStoreChange);
        this.localeStoreChange(LocaleStore.getState());

        if (!LocaleStore.getState().locales || LocaleStore.getState().locales.length === 0) {
            LocaleActions.getLocales();
        }

        document.addEventListener("mousedown", this.handleClickOutside);
    }

    componentDidUpdate() {
        if (
            this.props.selectedLocale &&
            Object.keys(this.props.selectedLocale).length === 0
        ) {
            const defaultLocale = this.getDefaultLocale();
            if (defaultLocale) {
                this.handleSelect(defaultLocale);
            }
        }
    }

    componentWillUnmount() {
        LocaleStore.unlisten(this.localeStoreChange);
        document.removeEventListener("mousedown", this.handleClickOutside);
    }

    localeStoreChange = (state) => {
        const locales = state.locales || [];
        this.setState({ locales });
    };

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
                    ? Locales.getDisplayName(this.props.selectedLocale.bcp47Tag)
                    : prevState.searchTerm
            }));
        }
    };

    getDefaultLocale = () => {
        return this.state.locales.find(locale => locale.bcp47Tag === "en") || null;
    };

    render() {
        const { searchTerm, locales, open } = this.state;
        const filteredLocales = locales.filter(locale => {
            const displayString = `${Locales.getDisplayName(locale.bcp47Tag)} ${locale.bcp47Tag}`.toLowerCase();
            return displayString.includes(searchTerm.toLowerCase());
        });
        
        let inputValue = open
            ? searchTerm
            : (this.props.selectedLocale.bcp47Tag
                ? `${Locales.getDisplayName(this.props.selectedLocale.bcp47Tag)} ${this.props.selectedLocale.bcp47Tag}`
                : "");

        return (
            <div ref={this.dropdownRef} className="locale-dropdown-root">
                <input
                    type="text"
                    className="form-control locale-dropdown-input"
                    placeholder="Choose a locale"
                    value={inputValue}
                    onChange={this.handleInputChange}
                    onClick={this.handleInputClick}
                    autoComplete="off"
                />
                {open && (
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

export default SourceLocaleDropdown;
