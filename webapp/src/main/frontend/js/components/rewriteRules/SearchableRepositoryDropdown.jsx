import React from "react";
import PropTypes from "prop-types";
import {injectIntl, FormattedMessage} from 'react-intl';

class SearchableRepositoryDropdown extends React.Component {
    static propTypes = {
        onSelect: PropTypes.func.isRequired,
        selectedRepositoryId: PropTypes.oneOfType([PropTypes.string, PropTypes.number]),
        repositoryOptions: PropTypes.arrayOf(
            PropTypes.shape({
                name: PropTypes.string,
                id: PropTypes.number
            })
        ).isRequired,
        showNoneOption: PropTypes.bool,
    };

    constructor(props) {
        super(props);
        this.state = {
            searchTerm: "",
            isOpen: false,
        };
        this.dropdownRef = React.createRef();
        this._filteredCache = { repositoryOptions: null, searchTerm: null, result: [] };
    }

    componentDidMount() {
        document.addEventListener("mousedown", this.handleClickOutside);
    }

    componentWillUnmount() {
        document.removeEventListener("mousedown", this.handleClickOutside);
    }

    componentDidUpdate(prevProps) {
        if (prevProps.selectedRepositoryId !== this.props.selectedRepositoryId) {
            if (!this.props.selectedRepositoryId) {
                this.setState({ searchTerm: "" });
            }
        }
    }

    getSelectedRepository = () => {
        if (!this.props.selectedRepositoryId) return null;
        return this.props.repositoryOptions.find(
            r => r.id === Number(this.props.selectedRepositoryId)
        ) || null;
    };

    handleSelect = (repo) => {
        this.setState({ isOpen: false, searchTerm: repo ? repo.name : "" });
        this.props.onSelect(repo);
    };

    handleInputChange = (e) => {
        this.setState({ searchTerm: e.target.value, isOpen: true });
    };

    handleInputClick = () => {
        this.setState({ isOpen: true });
    };

    handleClickOutside = (event) => {
        if (this.dropdownRef.current && !this.dropdownRef.current.contains(event.target)) {
            const selectedRepo = this.getSelectedRepository();
            let searchTerm = "";
            if (selectedRepo) {
                searchTerm = selectedRepo.name;
            } else if (this.props.showNoneOption) {
                searchTerm = "";
            }
            this.setState({
                isOpen: false,
                searchTerm
            });
        }
    };

    getInputValue = () => {
        if (this.state.isOpen) {
            return this.state.searchTerm;
        }
        const selectedRepo = this.getSelectedRepository();
        if (selectedRepo) {
            return selectedRepo.name;
        }
        if (this.props.showNoneOption && !this.props.selectedRepositoryId) {
            return this.props.intl.formatMessage({id: "rewriteRules.repositoryDropdown.none"});
        }
        return "";
    };

    getFilteredRepositories = () => {
        const { repositoryOptions } = this.props;
        const { searchTerm } = this.state;
        if (this._filteredCache.repositoryOptions === repositoryOptions && this._filteredCache.searchTerm === searchTerm) {
            return this._filteredCache.result;
        }
        const result = repositoryOptions.filter(repo => {
            return repo.name.toLowerCase().includes(searchTerm.toLowerCase());
        });
        this._filteredCache = { repositoryOptions, searchTerm, result };
        return result;
    };

    handleRepoClick = (e) => {
        const repoId = Number(e.currentTarget.dataset.id);
        const repo = this.props.repositoryOptions.find(r => r.id === repoId);
        if (repo) this.handleSelect(repo);
    };

    render() {
        const filteredRepos = this.getFilteredRepositories();
        return (
            <div ref={this.dropdownRef} className="modal-dropdown-root">
                <input
                    type="text"
                    className="form-control searchable-dropdown-input"
                    placeholder={this.props.intl.formatMessage({id: "rewriteRules.repositoryDropdown.placeholder"})}
                    value={this.getInputValue()}
                    onChange={this.handleInputChange}
                    onClick={this.handleInputClick}
                    autoComplete="off"
                />
                {this.state.isOpen && (
                    <ul className="dropdown-menu searchable-dropdown-menu">
                        {this.props.showNoneOption && !this.state.searchTerm && (
                            <li
                                className="searchable-dropdown-item"
                                onClick={() => this.handleSelect(null)}
                            >
                                <a><FormattedMessage id="rewriteRules.repositoryDropdown.none"/></a>
                            </li>
                        )}
                        {filteredRepos.length > 0 ? (
                            filteredRepos.map(repo => (
                                <li
                                    key={repo.id}
                                    data-id={repo.id}
                                    className="searchable-dropdown-item"
                                    onClick={this.handleRepoClick}
                                >
                                    <a>{repo.name}</a>
                                </li>
                            ))
                        ) : (
                            <li className="disabled searchable-dropdown-item-disabled">
                                <a><FormattedMessage id="rewriteRules.repositoryDropdown.noResults"/></a>
                            </li>
                        )}
                    </ul>
                )}
            </div>
        );
    }
}

export default injectIntl(SearchableRepositoryDropdown);
