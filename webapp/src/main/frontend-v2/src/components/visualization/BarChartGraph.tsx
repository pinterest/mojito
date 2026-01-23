import { memo } from "react";
import {
    BarElement,
    CategoryScale,
    Chart as ChartJS,
    Legend,
    LinearScale,
    Tooltip,
} from "chart.js";
import { Bar, type ChartProps } from "react-chartjs-2";

export type BarChartGraphProps = Omit<ChartProps<"bar">, "type">;

ChartJS.register(CategoryScale, LinearScale, BarElement, Tooltip, Legend);

const BarChartGraph: React.FC<BarChartGraphProps> = ({ data, options }) => {
    const chartData = { ...data, type: "bar" };

    return <Bar data={chartData} options={options} />;
};

export default memo(BarChartGraph);
