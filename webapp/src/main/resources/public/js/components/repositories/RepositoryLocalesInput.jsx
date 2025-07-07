import React, { useEffect } from "react";

const RepositoryLocalesInput = ({
    repositoryLocales,
    onRepositoryLocalesChange
}) => {
    
    useEffect(() => {
        console.log("repositoryLocales", repositoryLocales);
    }, [repositoryLocales]);

    return (
        <div className="form-group">
            {repositoryLocales.map((locale, index) => (
                <div key={index} className="mbm">
                    {locale.name}
                </div>
            ))}
        </div>
    );
};

export default RepositoryLocalesInput;
