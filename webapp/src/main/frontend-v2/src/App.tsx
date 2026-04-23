import { Route, Routes } from "react-router";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";

import { App as AntApp, ConfigProvider } from "antd";
import "./App.css";
import Navbar from "./components/navigation/Navbar";
import WorkInProgress from "./components/WorkInProgress";
import BranchesPage from "./features/branches/Branches";
import BranchPage from "./features/branch/BranchPage";

const mojitoGreen = "#559745";
const mojitoLightGreen = "#dbedd7";

// Create queryClient outside of the App component to avoid re-creating it on every render, which would invalidate the cache
const queryClient = new QueryClient();

function App() {
  return (
    <ConfigProvider
      theme={{
        token: {
          colorPrimary: mojitoGreen,
        },
        components: {
          Table: {
            rowSelectedBg: mojitoLightGreen,
            rowHoverBg: mojitoLightGreen,
            rowSelectedHoverBg: mojitoLightGreen,
          },
          Select: {
            optionSelectedBg: mojitoLightGreen,
          },
        },
      }}
    >
      <AntApp>
        <QueryClientProvider client={queryClient}>
          <Navbar />

          <Routes>
            <Route path='/' element={<WorkInProgress />} />
            <Route path='/branch' element={<BranchPage />} />
            <Route path='/branches' element={<BranchesPage />} />
          </Routes>
        </QueryClientProvider>
      </AntApp>
    </ConfigProvider>
  );
}

export default App;
