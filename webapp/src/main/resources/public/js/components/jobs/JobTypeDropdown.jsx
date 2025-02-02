import React from "react";
import createReactClass from 'create-react-class';
import {FormattedMessage, injectIntl} from 'react-intl';
import {DropdownButton, MenuItem} from "react-bootstrap";

let JobTypeDropDown = createReactClass({
    displayName: 'JobTypeDropDown',

    /**
     * Currently there is no way to prevent the dropdown to close on select
     * unless using a trick based on this attribute.
     *
     * Action that shouldn't close the dropdown can set this attribute to 'true'
     * This will prevent onDropdownToggle to actually close the dropdown.
     * Subsequent calls to onDropdownToggle will behave normally.
     */
    forceDropdownOpen: false,

    /**
     * @return {JSX.Element}
     */
    render() {
        return (
                <span className="mlm locale-dropdown">
                    <DropdownButton id="JobTypeDropdown" disabled={true} title={"THIRD_PARTY_SYNC"}>
                        <MenuItem divider/>
                        <MenuItem active={true}>Third Party Sync</MenuItem>
                    </DropdownButton>
                </span>
        );

    },
});

export default injectIntl(JobTypeDropDown);
