import React from "react";
import createReactClass from 'create-react-class';
import {withRouter} from "react-router";
import JobTypeDropdown from "./JobTypeDropdown";
import JobsView from "./JobsView";

let Jobs = createReactClass({
    displayName: 'Jobs',
    componentWillUnmount() {},
    render: function () {
        const clearLeftFix = {
            clear: 'left',
        };
        return (
            <div>
                <div className="pull-left">
                    <JobTypeDropdown />
                </div>

                <div className="mtl mbl" style={clearLeftFix}>
                    <JobsView />
                </div>
            </div>
        );
    },
});

export default withRouter(Jobs);
