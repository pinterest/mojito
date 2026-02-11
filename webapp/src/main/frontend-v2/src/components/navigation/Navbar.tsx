import { Menu, type MenuProps } from "antd";
import { memo, useMemo } from "react";
import { useLocation, useNavigate } from "react-router";

import favicon from "@/assets/favicon.ico";

type MenuItem = Required<MenuProps>["items"][number];

const Navbar: React.FC = () => {
  const navigate = useNavigate();
  const location = useLocation();

  const selectedKeys = useMemo(() => {
    if (location.pathname === "/branches") return ["branches"];
  }, [location.pathname]);

  const menuItems: MenuItem[] = [
    {
      label: "Mojito",
      key: "home",
      icon: <img src={favicon} alt='favicon' width={16} height={16} />,
      onClick: async () => {
        // navigating to the older version of the app.
        // we cannot use react router to navigate there because it's a different app
        window.location.assign("/");
      },
    },
    {
      label: "Branches",
      key: "branches",
      onClick: async () => {
        await navigate("/branches");
      },
    },
  ];

  return (
    <Menu selectedKeys={selectedKeys} items={menuItems} mode='horizontal' />
  );
};

export default memo(Navbar);
