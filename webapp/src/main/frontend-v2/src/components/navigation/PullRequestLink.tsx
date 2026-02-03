import React, { useMemo } from "react";
import { interpolateLink } from "./utils/interpolateLink";

interface PullRequestLinkProps {
  repoName: string;
  branchName: string;
  children?: React.ReactNode;
  className?: string;
}

const PullRequestLink: React.FC<PullRequestLinkProps> = ({
  repoName,
  branchName,
  children,
  className,
}) => {
  const url = useMemo(() => {
    return APP_CONFIG.link[repoName]?.pullRequest?.url
      ? interpolateLink(APP_CONFIG.link[repoName].pullRequest.url, {
          branch: encodeURIComponent(branchName),
        })
      : null;
  }, [repoName, branchName]);

  if (!url) {
    return <div className={className}>{children || branchName}</div>;
  }

  return (
    <a
      href={url}
      target='_blank'
      rel='noopener noreferrer'
      className={className}
    >
      {children || branchName}
    </a>
  );
};

export default PullRequestLink;
