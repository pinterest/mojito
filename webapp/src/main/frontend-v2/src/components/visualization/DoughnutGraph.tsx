import { memo } from "react";
import { ArcElement, Chart as ChartJS, Legend, Tooltip } from "chart.js";
import { type ChartProps, Doughnut } from "react-chartjs-2";

export type DoughnutGraphProps = Omit<ChartProps<"doughnut">, "type">;

ChartJS.register(ArcElement, Tooltip, Legend);

const DoughnutGraph: React.FC<DoughnutGraphProps> = ({ data }) => {
  const chartData = { ...data, type: "doughnut" };

  return <Doughnut data={chartData} />;
};

export default memo(DoughnutGraph);
