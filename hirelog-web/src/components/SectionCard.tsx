type SectionCardProps = {
  title: string;
  children: string;
};

export default function SectionCard({ title, children }: SectionCardProps) {
  return (
    <section className="rounded-lg border border-gray-200 bg-white p-6 shadow-sm">
      <h2 className="mb-3 text-base font-semibold text-gray-900">{title}</h2>

      <div className="whitespace-pre-line leading-relaxed text-gray-700 text-sm">
        {children}
      </div>
    </section>
  );
}
