import { Route, Routes } from "react-router";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";

import { App as AntApp, ConfigProvider } from "antd";
import "./App.css";
import Navbar from "./components/navigation/Navbar";
import WorkInProgress from "./components/WorkInProgress";
import BranchPage from "@/features/Branch/BranchPage";

const mojitoGreen = "#559745";

function App() {
    const queryClient = new QueryClient();

    return (
        <ConfigProvider
            theme={{
                token: {
                    colorPrimary: mojitoGreen,
                },
            }}
        >
            <AntApp>
                <QueryClientProvider client={queryClient}>
                    <Navbar />

                    <Routes>
                        <Route path="/" element={<WorkInProgress />} />
                        <Route path="/branch" element={<BranchPage />} />
                    </Routes>
                </QueryClientProvider>
            </AntApp>
        </ConfigProvider>
    );
}

export default App;
