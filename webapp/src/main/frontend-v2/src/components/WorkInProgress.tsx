import { Card, Progress, Result, Space, theme, Typography } from "antd";
import { ToolOutlined } from "@ant-design/icons";
import { useTranslation } from "react-i18next";

const { Title, Paragraph } = Typography;

export default function WorkInProgress() {
  const { token } = theme.useToken();
  const { t } = useTranslation("common");

  return (
    <Card
      variant='outlined'
      style={{
        maxWidth: 600,
        margin: "auto",
        marginTop: "5vh",
        textAlign: "center",
      }}
    >
      <Result
        icon={<ToolOutlined style={{ color: token.colorPrimary }} />}
        title={<Title level={2}>{t("comingSoon")}</Title>}
        subTitle={
          <Space orientation='vertical' size='large'>
            <Paragraph>{t("siteUnderConstruction")}</Paragraph>

            <Paragraph className='text-sm text-gray-500 mb-2'>
              {t("progressMessage")}
            </Paragraph>

            <Progress
              percent={10}
              status='active'
              strokeColor={{
                "0%": "#BCB382",
                "100%": token.colorPrimary,
              }}
            />
          </Space>
        }
      />
    </Card>
  );
}
