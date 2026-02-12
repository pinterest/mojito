import { Avatar, Flex, Menu, type MenuProps } from "antd";
import { memo, useMemo } from "react";
import { useLocation, useNavigate } from "react-router";

import favicon from "@/assets/favicon.ico";
import type { AppConfig } from "@/types/appConfig";

type MenuItem = Required<MenuProps>["items"][number];

function getAccountName(user: AppConfig["user"]): string {
  return user?.username?.charAt(0).toUpperCase() ?? "";
}

const Navbar: React.FC = () => {
  const navigate = useNavigate();
  const location = useLocation();

  const selectedKeys = useMemo(() => {
    if (location.pathname === "/branches") return ["branches"];
  }, [location.pathname]);

  const navItems: MenuItem[] = [
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
    <Flex
      orientation='horizontal'
      className='nav-container'
      justify='space-between'
      align='center'
    >
      <Menu
        className='navbar'
        selectedKeys={selectedKeys}
        items={navItems}
        mode='horizontal'
      />

      <div className='avatar-container'>
        <Avatar>{getAccountName(APP_CONFIG.user)}</Avatar>
      </div>
    </Flex>
  );
};

export default memo(Navbar);
