import React from "react";
import JobRepositoryDropDown from "./JobRepositoryDropDown";
import JobTypeDropdown from "./JobTypeDropdown";
import LabeledTextInput from "./LabeledTextInput";

const JobGeneralInput = ({ selectedRepository, onRepositorySelect, jobType, onJobTypeChange, cron, onInputChange }) => (
    <div>
        <div className="form-group mbm">
            <label>Repository*</label>
            <JobRepositoryDropDown
                selected={selectedRepository}
                onSelect={onRepositorySelect}
            />
        </div>
        <div className="form-group mtm mbm">
            <label>Job Type*</label>
            <JobTypeDropdown jobType={jobType} onJobTypeChange={onJobTypeChange} />
        </div>
        <LabeledTextInput
            label="Sync Frequency (Cron)*"
            placeholder="Enter cron expression"
            inputName="cron"
            value={cron}
            onChange={onInputChange}
        />
    </div>
);

export default JobGeneralInput;
