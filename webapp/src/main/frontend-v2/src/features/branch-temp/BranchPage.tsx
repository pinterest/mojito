import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { Flex, message, Result, Spin } from "antd";
import React, { useState } from "react";

import "./index.css";

import { getBranchStatistics } from "@/api/getBranchStatistics";
import { getBranchTextUnitStatus } from "@/api/getBranchTextUnitStatus";
import type { BranchTextUnitStatusDto } from "@/api/types/branchTextUnitStatus";
import BranchDetails from "@/features/Branch/BranchDetails";
import { useBranchName } from "@/hooks/useBranchName";
import { useRepoName } from "@/hooks/useRepoName";
import type { BranchStatistics } from "@/types/branchStatistics";
import type { Page } from "@/types/page";
import { deleteBranch } from "@/api/deleteBranch";
import type { PollableTask } from "@/api/types/pollableTask";
import { getPollableTask } from "@/api/getPollableTask";

const BranchPage: React.FC = () => {
  const branchName = useBranchName();
  const repoName = useRepoName();
  const queryClient = useQueryClient();

  const [pendingTaskId, setPendingTaskId] = useState<number | null>(null);

  const branchTextUnitStatus = useQuery<BranchTextUnitStatusDto>({
    queryKey: ["branchTextUnitStatus", { branchName, repoName }],
    queryFn: () =>
      getBranchTextUnitStatus({
        branchName,
        repoName,
      }),
  });

  const branchStatsQuery = useQuery<Page<BranchStatistics>>({
    queryKey: ["branchStatistics", { branchName }],
    queryFn: () =>
      getBranchStatistics({
        branchId: branchTextUnitStatus.data!.branchId,
      }),
    enabled: branchTextUnitStatus.isSuccess,
  });

  const pendingDeleteQuery = useQuery<PollableTask>({
    queryKey: ["pollableTask", { pendingTaskId }],
    queryFn: () => getPollableTask({ pollableTaskId: pendingTaskId! }),
    refetchInterval: (query) => {
      const isDone = query.state.data?.allFinished;
      if (isDone) {
        queryClient
          .invalidateQueries({
            queryKey: ["branchStatistics", { branchName }],
          })
          .then(() => {
            setPendingTaskId(null);
          });
        return false;
      }

      return 200;
    },
    enabled: pendingTaskId !== null,
  });

  const deleteBranchMutation = useMutation({
    mutationFn: async ({
      branchId,
      repositoryId,
    }: {
      branchId: number;
      repositoryId: number;
    }) => {
      const task = await deleteBranch({ branchId, repositoryId });
      if (task.id) {
        setPendingTaskId(task.id);
      }
    },
    onError: (error) => {
      message.error("Failed to delete branch. Please try again.");
      console.error("Error deleting branch:", error);
    },
  });

  if (
    branchStatsQuery.isLoading ||
    branchTextUnitStatus.isLoading ||
    deleteBranchMutation.isPending ||
    pendingDeleteQuery.isLoading ||
    pendingTaskId !== null
  ) {
    return (
      <Flex justify='center' style={{ margin: "2vh" }}>
        <Spin />
      </Flex>
    );
  }

  const branch = branchStatsQuery.data?.content?.[0];
  if (
    branchStatsQuery.isError ||
    branchTextUnitStatus.isError ||
    !branch ||
    !branchTextUnitStatus.data
  ) {
    return (
      <Result
        status='500'
        subTitle='Branch details could not be loaded. Please try again later.'
      />
    );
  }

  return (
    <BranchDetails
      branchStats={branch}
      branchTextUnitStatus={branchTextUnitStatus.data}
      onBranchDelete={(branchId, repositoryId) => {
        deleteBranchMutation.mutate({
          branchId: branchId,
          repositoryId: repositoryId,
        });
      }}
    />
  );
};

export default BranchPage;
