import type { Metadata } from "next";
import "./globals.css";

export const metadata: Metadata = {
  title: "길눈 AI | 안전한 복지 신청 길찾기",
  description:
    "개인정보와 자동 실행 없이 막힌 동작의 의미만 연습하는 복지 신청 길찾기 데모",
  icons: {
    icon: "/favicon.svg",
    shortcut: "/favicon.svg",
  },
};

export default function RootLayout({
  children,
}: Readonly<{
  children: React.ReactNode;
}>) {
  return (
    <html lang="ko">
      <body>{children}</body>
    </html>
  );
}
