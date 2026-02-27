import { useState, useEffect } from 'react';
import { useAuthStore } from '../store/authStore';
import { memberService } from '../services/memberService';
import { TbEdit, TbX } from 'react-icons/tb';

const ProfilePage = () => {
  const { user, setUser } = useAuthStore();

  const [isModalOpen, setIsModalOpen] = useState(false);
  const [isLoading, setIsLoading] = useState(false);
  const [successMessage, setSuccessMessage] = useState('');

  const [form, setForm] = useState({
    username: '',
    email: '',
    careerYears: 0,
    summary: '',
  });

  useEffect(() => {
    if (user) {
      setForm({
        username: user.username || '',
        email: user.email || '',
        careerYears: user.careerYears || 0,
        summary: user.summary || '',
      });
    }
  }, [user]);

  if (!user) {
    return (
      <div className="min-h-screen flex items-center justify-center">
        <p className="text-gray-400 text-sm">프로필 정보를 불러오는 중...</p>
      </div>
    );
  }

  const handleSave = async () => {
    setIsLoading(true);
    try {
      await memberService.updateUsername({ username: form.username });

      await memberService.updateProfile({
        careerYears: form.careerYears,
        summary: form.summary,
      });

      if (form.email !== user.email) {
        await memberService.updateEmail({ email: form.email });
      }

      const updated = await memberService.getMe();
      setUser(updated);

      setSuccessMessage('정보가 수정되었습니다.');

      setTimeout(() => {
        setSuccessMessage('');
        setIsModalOpen(false);
      }, 1500);
    } catch (err) {
      setSuccessMessage('수정 중 오류가 발생했습니다.');
    } finally {
      setIsLoading(false);
    }
  };

  return (
    <div className="min-h-screen bg-[#F8FBFC] pt-24 pb-20 px-6">
      <div className="max-w-5xl mx-auto space-y-10">

        {/* 상단 프로필 카드 */}
        <div className="relative rounded-3xl p-12 text-white overflow-hidden
          bg-gradient-to-r from-[#4CDFD5] to-[#36C9C6]
          shadow-[0_20px_50px_-10px_rgba(76,223,213,0.35)]">

          <button
            onClick={() => setIsModalOpen(true)}
            className="absolute top-6 right-6 bg-white/20 hover:bg-white/30 px-4 py-2 rounded-xl text-sm flex items-center gap-2 transition backdrop-blur-sm"
          >
            <TbEdit size={16} />
            정보 수정
          </button>

          <div className="flex items-center gap-10">
            <div className="w-28 h-28 rounded-full bg-white/30 flex items-center justify-center text-4xl font-bold backdrop-blur-sm">
              {user.username?.charAt(0).toUpperCase()}
            </div>

            <div>
              <h2 className="text-3xl font-bold tracking-tight">
                {user.username}
              </h2>
              <p className="text-white/90">{user.email}</p>
              <p className="text-white/80 text-sm mt-2">
                {user.currentPosition?.name || '직무 정보 없음'}
              </p>
            </div>
          </div>
        </div>

        {/* 상세 정보 카드 */}
        <div className="bg-white rounded-3xl border border-[#4CDFD5]/20 p-10 shadow-sm">

          <div className="grid grid-cols-1 md:grid-cols-2 gap-8">

            <InfoRow label="User ID" value={user.id} />
            <InfoRow label="Role" value={user.role} />
            <InfoRow label="Status" value={user.status} />
            <InfoRow
              label="경력"
              value={
                user.careerYears
                  ? `${user.careerYears}년`
                  : '신입'
              }
            />
            <InfoRow
              label="가입일"
              value={user.createdAt?.slice(0, 10)}
            />
          </div>

          <div className="mt-10">
            <p className="text-sm text-gray-500 mb-3">자기소개</p>
            <div className="bg-[#4CDFD5]/5 border border-[#4CDFD5]/20 rounded-2xl p-6 text-gray-700 text-sm leading-relaxed">
              {user.summary || '작성된 소개가 없습니다.'}
            </div>
          </div>
        </div>
      </div>

      {/* 수정 모달 */}
      {isModalOpen && (
        <div className="fixed inset-0 bg-black/40 backdrop-blur-sm flex items-center justify-center z-50">
          <div className="bg-white w-full max-w-lg rounded-3xl p-8 relative shadow-xl">

            <button
              onClick={() => setIsModalOpen(false)}
              className="absolute top-4 right-4 text-gray-400 hover:text-gray-700"
            >
              <TbX size={20} />
            </button>

            <h2 className="text-xl font-bold mb-6 text-[#4CDFD5]">
              정보 수정
            </h2>

            {successMessage && (
              <div className="mb-4 text-sm text-[#4CDFD5] font-semibold">
                {successMessage}
              </div>
            )}

            <div className="space-y-4">

              <Input
                label="닉네임"
                value={form.username}
                onChange={(v: string) =>
                  setForm({ ...form, username: v })
                }
              />

              <Input
                label="이메일"
                value={form.email}
                onChange={(v: string) =>
                  setForm({ ...form, email: v })
                }
              />

              <Input
                label="경력(년)"
                type="number"
                value={form.careerYears}
                onChange={(v: string) =>
                  setForm({
                    ...form,
                    careerYears: parseInt(v) || 0,
                  })
                }
              />

              <Input
                label="자기소개"
                value={form.summary}
                onChange={(v: string) =>
                  setForm({ ...form, summary: v })
                }
              />

              <button
                onClick={handleSave}
                disabled={isLoading}
                className="w-full py-3 bg-[#4CDFD5] hover:bg-[#36C9C6]
                  text-white rounded-2xl font-semibold transition shadow-md"
              >
                {isLoading ? '저장 중...' : '저장하기'}
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
};

const InfoRow = ({ label, value }: any) => (
  <div className="flex justify-between text-sm border-b border-gray-100 pb-3">
    <span className="text-gray-500">{label}</span>
    <span className="font-semibold text-gray-800">{value}</span>
  </div>
);

const Input = ({ label, value, onChange, type = 'text' }: any) => (
  <div>
    <label className="text-sm text-gray-500">{label}</label>
    <input
      type={type}
      value={value}
      onChange={(e) => onChange(e.target.value)}
      className="w-full mt-1 px-4 py-3 rounded-2xl border border-gray-200
        focus:border-[#4CDFD5] focus:ring-4 focus:ring-[#4CDFD5]/20 outline-none transition"
    />
  </div>
);

export default ProfilePage;
