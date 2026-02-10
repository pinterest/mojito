import { Menu, type MenuProps } from "antd";
import { memo } from "react";
import { useNavigate } from "react-router";

import favicon from "@/assets/favicon.ico";

type MenuItem = Required<MenuProps>["items"][number];

const Navbar: React.FC = () => {
  const navigate = useNavigate();

  const menuItems: MenuItem[] = [
    {
      label: "Mojito",
      key: "home",
      icon: <img src={favicon} alt='favicon' width={16} height={16} />,
    },
    {
      label: "Branches",
      key: "branches",
      onClick: () => {
        navigate("/branches");
      },
    },
  ];

  return <Menu items={menuItems} mode='horizontal' />;
};

export default memo(Navbar);
